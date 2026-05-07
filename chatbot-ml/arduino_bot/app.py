
from fastapi import FastAPI
from pydantic import BaseModel
import uvicorn
import os

# ✅ Resolve absolute model path (robust fix)
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MODEL_PATH = os.path.join(BASE_DIR, "arduino_fixed_model")

print("MODEL PATH:", MODEL_PATH)
print("MODEL EXISTS:", os.path.exists(MODEL_PATH))

# ✅ Safe import
try:
    from transformers import AutoTokenizer, AutoModelForSeq2SeqLM
    MODEL_EXISTS = os.path.exists(MODEL_PATH)
except ImportError:
    MODEL_EXISTS = False

from fastapi.middleware.cors import CORSMiddleware

app = FastAPI(title="Arduino Chatbot API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# ✅ Request & Response schemas
class ChatRequest(BaseModel):
    instruction: str
    code: str

class ChatResponse(BaseModel):
    fixed_code: str
    explanation: str

# ✅ Load model safely — try local fine-tuned, fall back to HuggingFace T5-base
tokenizer, model = None, None
USING_FALLBACK = False

if MODEL_EXISTS:
    try:
        print("Loading fine-tuned Arduino model...")
        tokenizer = AutoTokenizer.from_pretrained(MODEL_PATH)
        model = AutoModelForSeq2SeqLM.from_pretrained(MODEL_PATH)
        print("Fine-tuned model loaded successfully!")
    except Exception as e:
        print(f"Failed to load fine-tuned model: {e}")
        MODEL_EXISTS = False

if not MODEL_EXISTS:
    try:
        print("Fine-tuned weights not found. Falling back to t5-small from HuggingFace...")
        tokenizer = AutoTokenizer.from_pretrained("t5-small")
        model = AutoModelForSeq2SeqLM.from_pretrained("t5-small")
        MODEL_EXISTS = True
        USING_FALLBACK = True
        print("Fallback t5-small model loaded successfully!")
    except Exception as e:
        print(f"Failed to load fallback model: {e}")
        MODEL_EXISTS = False

# ✅ Explanation generator
def generate_explanation(original_code, fixed_code):
    import re

    orig_norm = " ".join(original_code.split())
    fixed_norm = " ".join(fixed_code.split())

    if orig_norm == fixed_norm:
        return "The AI model found no errors in your code. It looks correct!"

    def to_lines(code):
        code = re.sub(r';', ';\n', code)
        code = re.sub(r'\{', '{\n', code)
        code = re.sub(r'\}', '}\n', code)
        return [l.strip() for l in code.split('\n') if l.strip()]

    orig_lines = to_lines(orig_norm)
    fixed_lines = to_lines(fixed_norm)

    m, n = len(orig_lines), len(fixed_lines)
    dp = [[0]*(n+1) for _ in range(m+1)]

    for i in range(1, m+1):
        for j in range(1, n+1):
            if orig_lines[i-1] == fixed_lines[j-1]:
                dp[i][j] = dp[i-1][j-1] + 1
            else:
                dp[i][j] = max(dp[i-1][j], dp[i][j-1])

    diff = []
    i, j = m, n

    while i > 0 or j > 0:
        if i > 0 and j > 0 and orig_lines[i-1] == fixed_lines[j-1]:
            diff.append(('same', orig_lines[i-1]))
            i -= 1
            j -= 1
        elif j > 0 and (i == 0 or dp[i][j-1] >= dp[i-1][j]):
            diff.append(('add', fixed_lines[j-1]))
            j -= 1
        else:
            diff.append(('del', orig_lines[i-1]))
            i -= 1

    diff.reverse()

    changes = []
    removed = [d[1] for d in diff if d[0] == 'del']
    added = [d[1] for d in diff if d[0] == 'add']

    used_add = set()

    for r in removed:
        best_match = -1
        best_score = 0

        for idx, a in enumerate(added):
            if idx in used_add:
                continue

            r_words = set(re.findall(r'[a-zA-Z_]\w*', r))
            a_words = set(re.findall(r'[a-zA-Z_]\w*', a))
            common = len(r_words & a_words)

            if common > best_score and common >= 1:
                best_score = common
                best_match = idx

        if best_match >= 0:
            changes.append(f"Changed: '{r}' → '{added[best_match]}'")
            used_add.add(best_match)
        else:
            changes.append(f"Removed: '{r}'")

    for idx, a in enumerate(added):
        if idx not in used_add:
            changes.append(f"Added: '{a}'")

    if changes:
        explanation = "The AI model found and fixed the following issues:\n- " + "\n- ".join(changes)
    else:
        explanation = "The AI model analyzed and corrected the code."

    return explanation

# ✅ API endpoint
@app.post("/api/chat", response_model=ChatResponse)
async def generate_fix(req: ChatRequest):
    if MODEL_EXISTS:
        try:
            prompt = f"fix: {req.code}"

            inputs = tokenizer(
                prompt,
                return_tensors="pt",
                max_length=512,
                truncation=True
            )

            outputs = model.generate(
                **inputs,
                max_length=512,
                num_beams=5,
                early_stopping=True,
                no_repeat_ngram_size=0,
            )

            fixed_code = tokenizer.decode(outputs[0], skip_special_tokens=True).strip()
            explanation = generate_explanation(req.code, fixed_code)

            if USING_FALLBACK:
                explanation = "[Note: Using generic T5 model — fine-tuned Arduino weights not found. Results may be limited.]\n\n" + explanation

            return ChatResponse(
                fixed_code=fixed_code,
                explanation=explanation
            )

        except Exception as e:
            return ChatResponse(
                fixed_code="",
                explanation=f"Model error: {str(e)}"
            )
    else:
        return ChatResponse(
            fixed_code="// Model unavailable",
            explanation="⚠️ The AI model could not be loaded. Please ensure the model files are present in the arduino_fixed_model directory."
        )

# ✅ Run server
if __name__ == "__main__":
    print("Starting ML API on port 8000...")
    uvicorn.run(app, host="0.0.0.0", port=8000)


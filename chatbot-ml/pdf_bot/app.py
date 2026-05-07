import os
from fastapi import FastAPI, Request, Form
from fastapi.templating import Jinja2Templates
from fastapi.responses import HTMLResponse, PlainTextResponse
from fastapi.middleware.cors import CORSMiddleware
import uvicorn
from dotenv import load_dotenv
from langchain_groq import ChatGroq
from langchain_pinecone import PineconeVectorStore
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.runnables import RunnablePassthrough
from langchain_core.output_parsers import StrOutputParser

try:
    from src.helper import download_hugging_face_embedding
    from src.prompt import system_prompt
except ImportError as e:
    print(f"[WARNING] Could not import src modules: {e}")
    download_hugging_face_embedding = None
    system_prompt = "You are a helpful assistant. Context: {context}"


app = FastAPI(title="PDF Chatbot API")
templates = Jinja2Templates(directory="templates")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# Load environment variables
load_dotenv()
PINECONE_API_KEY = os.environ.get('PINECONE_API_KEY')
GROQ_API_KEY = os.environ.get('GROQ_API_KEY')

# Set API keys in environment safely
if PINECONE_API_KEY:
    os.environ["PINECONE_API_KEY"] = PINECONE_API_KEY
if GROQ_API_KEY:
    os.environ["GROQ_API_KEY"] = GROQ_API_KEY

# Initialize embeddings, Pinecone, and RAG chain only if dependencies are available
rag_chain = None

try:
    if download_hugging_face_embedding is not None and PINECONE_API_KEY and GROQ_API_KEY:
        embeddings = download_hugging_face_embedding()

        index_name = "hardwarebot"
        docsearch = PineconeVectorStore.from_existing_index(
            index_name=index_name,
            embedding=embeddings
        )

        retriever = docsearch.as_retriever(search_type="similarity", search_kwargs={"k": 3})

        llm = ChatGroq(
            groq_api_key=GROQ_API_KEY,
            model_name="llama-3.3-70b-versatile",
            temperature=0.4,
            max_tokens=500
        )
        prompt = ChatPromptTemplate.from_template(system_prompt + "\n\nQuestion: {input}")

        def format_docs(docs):
            return "\n\n".join(doc.page_content for doc in docs)

        rag_chain = (
            {"context": retriever | format_docs, "input": RunnablePassthrough()}
            | prompt
            | llm
            | StrOutputParser()
        )
        print("[INFO] PDF RAG chain initialized successfully!")
    else:
        print("[WARNING] Missing API keys or src modules. PDF RAG chain is disabled.")
except Exception as e:
    print(f"[ERROR] Failed to initialize PDF RAG chain: {e}")

@app.get("/", response_class=HTMLResponse)
async def index(request: Request):
    return templates.TemplateResponse("chat.html", {"request": request})

@app.post("/get", response_class=PlainTextResponse)
async def chat(msg: str = Form(...)):
    try:
        if rag_chain is None:
            return "The PDF knowledge model is not initialized. Please check your API keys in the .env file."
        print(f"User Input: {msg}")
        
        # Invoke RAG chain
        bot_response = rag_chain.invoke(msg)
        
        print(f"Bot Response: {bot_response}")
        return str(bot_response)
    except Exception as e:
        print(f"Error: {e}")
        return str("I'm sorry, I'm having trouble processing that right now.")

if __name__ == '__main__':
    uvicorn.run(app, host="0.0.0.0", port=5000)

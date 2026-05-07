# Duinophile — AI-Powered Arduino Learning Platform

Duinophile is a full-stack web application designed to help beginners learn Arduino programming. It features an AI-powered chatbot that can automatically detect and fix errors in Arduino code, built using a fine-tuned CodeT5 NLP model trained on 2,750+ Arduino error examples.


## Getting Started

### Prerequisites

Make sure you have the following installed:

- Java 17 or higher
- Maven
- MongoDB (running on port 27017)
- Python 3.10 or higher

### 1. Clone the repository

```bash
git clone https://github.com/IT24103874/Duinophile.git
cd duinophile
```

### 2. Download the trained model weight file

The trained model weight file (`model.safetensors`, ~230 MB) exceeds GitHub's file size limit and needs to be downloaded separately.

**Download link:** [model.safetensors — OneDrive](https://mysliit-my.sharepoint.com/:u:/g/personal/it24102913_my_sliit_lk/IQBBejg-IB_WQLVpERVfTgSDAdkl0nZK1vlt1O8-F90YNac?e=GzwoUghttps://mysliit-my.sharepoint.com/:u:/g/personal/it24102913_my_sliit_lk/IQBBejg-IB_WQLVpERVfTgSDAdkl0nZK1vlt1O8-F90YNac?e=GzwoUg)

After downloading, place the file inside the `chatbot-ml/arduino_fixed_model/` directory:

```
chatbot-ml/
└── arduino_fixed_model/
    ├── config.json              ← already in repo
    ├── generation_config.json   ← already in repo
    ├── merges.txt               ← already in repo
    ├── model.safetensors        ← PLACE THE DOWNLOADED FILE HERE
    ├── special_tokens_map.json  ← already in repo
    ├── tokenizer.json           ← already in repo
    ├── tokenizer_config.json    ← already in repo
    └── vocab.json               ← already in repo
```

### 3. Start MongoDB

```bash
mongod
```

### 4. Install Python dependencies

```bash
cd chatbot-ml
pip install -r requirements.txt
```

If PyTorch installation fails, try the CPU-only version:
```bash
pip install torch --index-url https://download.pytorch.org/whl/cpu
```

### 5. Start the ML server

```bash
cd chatbot-ml
python app.py
```

Wait until you see:
```
Loading fine-tuned model...
INFO:     Uvicorn running on http://0.0.0.0:8000
```

Keep this terminal running.

### 6. Start the Spring Boot application

Open a new terminal:

```bash
cd duinophile
mvn spring-boot:run
```

### 7. Open the application

Navigate to `http://localhost:8080` in your browser. The AI chatbot can be accessed by clicking the robot icon in the bottom-right corner.

## AI Chatbot

The chatbot uses a fine-tuned CodeT5-small model (60M parameters) to identify and fix Arduino code errors. It supports 80+ error types including:

- Missing semicolons and brackets
- Incorrect function casing (`pinmode` → `pinMode`)
- Misspelled function names (`dely` → `delay`)
- Logical errors (assignment vs comparison, missing delay in loop)
- Wrong data types for `millis()`
- Array out of bounds errors

### Testing the chatbot

Paste this buggy code into the chatbot:

```arduino
void setup(){pinmode(13,OUTPUT);}void loop(){digitalwrite(13,HIGH);delay(1000);digitalwrite(13,LOW) delay(1000);}
```

The model will correct the function casing and add the missing semicolons.



## ML Training Pipeline

The model was trained on Kaggle using a T4 GPU. The pipeline consists of five stages:

1. **Dataset Cleaning** — Collected and cleaned 2,750 Arduino code repair examples
2. **Model Fine-tuning** — Configured CodeT5-small for sequence-to-sequence code repair
3. **Hyperparameter Tuning** — Optimized learning rate (3e-5), batch size (8), and epochs (25)
4. **Model Training** — Trained for 25 epochs, achieving a final loss of 0.11
5. **Model Evaluation** — Tested across syntax and logical error categories


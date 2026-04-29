# src/query.py
import os
from dotenv import load_dotenv
from langchain_openai import OpenAI
from langchain.chains import create_retrieval_chain
from langchain.chains.combine_documents import create_stuff_documents_chain
from langchain_core.prompts import ChatPromptTemplate
from langchain_pinecone import PineconeVectorStore
from src.helper import download_hugging_face_embedding
from src.prompt import system_prompt


load_dotenv()
OPENAI_API_KEY = os.environ.get("OPENAI_API_KEY")


embeddings = download_hugging_face_embedding()


index_name = "hardwarebot"
docsearch = PineconeVectorStore.from_existing_index(
    index_name=index_name,
    embedding=embeddings
)

# Step 3: Create retriever
retriever = docsearch.as_retriever(
    search_type="similarity",
    search_kwargs={"k": 3}
)

# Step 4: Setup LLM
llm = OpenAI(api_key=OPENAI_API_KEY, temperature=0.4, max_tokens=500)

# Step 5: Setup RAG chain
prompt = ChatPromptTemplate.from_messages([
    ("system", system_prompt),
    ("human", "{input}")
])
question_answer_chain = create_stuff_documents_chain(llm, prompt)
rag_chain = create_retrieval_chain(retriever, question_answer_chain)


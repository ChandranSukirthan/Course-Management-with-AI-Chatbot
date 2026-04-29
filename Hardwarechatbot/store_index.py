import os
from dotenv import load_dotenv
from src.helper import load_pdf_file, text_split, download_hugging_face_embedding
from langchain_pinecone import PineconeVectorStore
from pinecone import Pinecone, ServerlessSpec


load_dotenv()
PINECONE_API_KEY = os.environ.get("PINECONE_API_KEY")
os.environ["PINECONE_API_KEY"] = PINECONE_API_KEY

print("Loading data...")
extracted_data = load_pdf_file(data='Data/')

print("Splitting text into chunks...")
text_chunks = text_split(extracted_data)


print("Downloading embeddings...")
embeddings = download_hugging_face_embedding()

pc = Pinecone(api_key=PINECONE_API_KEY)
index_name = "hardwarebot"

if index_name not in pc.list_indexes().names():
    print(f"Creating index '{index_name}'...")
    pc.create_index(
        name=index_name,
        dimension=384,
        metric="cosine",
        spec=ServerlessSpec(cloud="aws", region="us-east-1")
    )

print(f"Uploading vectors to index '{index_name}'...")
docsearch = PineconeVectorStore.from_documents(
    documents=text_chunks,
    index_name=index_name,
    embedding=embeddings,
    pinecone_api_key=PINECONE_API_KEY
)

print("Indexing complete! Vectors successfully stored in Pinecone.")

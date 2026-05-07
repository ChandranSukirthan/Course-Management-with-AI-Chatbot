import os
from dotenv import load_dotenv
from src.helper import load_pdf_file, text_split, download_hugging_face_embedding
from langchain_pinecone import PineconeVectorStore
from pinecone import Pinecone, ServerlessSpec

# Load environment variables
load_dotenv()
PINECONE_API_KEY = os.environ.get("PINECONE_API_KEY")
os.environ["PINECONE_API_KEY"] = PINECONE_API_KEY

# Step 1: Load medical PDF data
print("Loading data...")
extracted_data = load_pdf_file(data='Data/')

# Step 2: Split data into manageable text chunks
print("Splitting text into chunks...")
text_chunks = text_split(extracted_data)

# Step 3: Download the embedding model from Hugging Face
print("Downloading embeddings...")
embeddings = download_hugging_face_embedding()

# Step 4: Initialize Pinecone client
pc = Pinecone(api_key=PINECONE_API_KEY)
index_name = "medicalbot"

# Step 5: Create a new Pinecone index if it doesn't already exist
if index_name not in pc.list_indexes().names():
    print(f"Creating index '{index_name}'...")
    pc.create_index(
        name=index_name,
        dimension=384,
        metric="cosine",
        spec=ServerlessSpec(cloud="aws", region="us-east-1")
    )

# Step 6: Embed each text chunk and store them in Pinecone
print(f"Uploading vectors to index '{index_name}'...")
docsearch = PineconeVectorStore.from_documents(
    documents=text_chunks,
    index_name=index_name,
    embedding=embeddings,
    pinecone_api_key=PINECONE_API_KEY
)

print("Indexing complete! Vectors successfully stored in Pinecone.")

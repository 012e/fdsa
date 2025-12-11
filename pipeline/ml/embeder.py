from dotenv import load_dotenv
from langchain_openai import OpenAIEmbeddings

load_dotenv()

embeder = OpenAIEmbeddings(model="text-embedding-3-large", dimensions=1024)

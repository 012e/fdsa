from dotenv import load_dotenv
from langchain_openai import ChatOpenAI

load_dotenv()

llm = ChatOpenAI(
    model="gpt-5-nano",
    # stream_usage=True,
    # temperature=None,
    # max_tokens=None,
    # timeout=None,
    # reasoning_effort="low",
    # max_retries=2,
    # api_key="...",  # If you prefer to pass api key in directly
    # base_url="...",
    # organization="...",
    # other params...
)


def summarize_code(code: str):
    result = llm.invoke(
        [
            (
                "system",
                """The LLM will function as a Search Engine Summarizer. Your core directive is to analyze the provided search query and the associated, delimited list of search snippets, then synthesize this information into a single, cohesive, and factually accurate paragraph that directly answers the user's query. The summary must be abstractive (rewritten, not just extracted sentences), maintain a neutral, objective tone, prioritize the most relevant and non-redundant facts, and strictly exclude any preamble, conversational language, or apologies. If the information provided is insufficient or contradictory, state this limitation clearly and concisely at the end of the paragraph without using phrases like "I cannot." You must not use the search query or snippet text verbatim unless absolutely necessary for a proper noun or citation. The final output must be ONE paragraph.""",
            ),
            ("user", code),
        ],
    )
    return result.content

from ml.llm import summarize_code


def main():
    print("Hello from workflow!")
    print(summarize_code("Hello world"))


if __name__ == "__main__":
    main()

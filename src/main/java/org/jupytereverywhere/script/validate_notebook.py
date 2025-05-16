import sys

import nbformat


def main():
    notebook_json = sys.stdin.read()
    try:
        nbformat.reads(notebook_json, as_version=4)  # type: ignore
        print("valid")
    except Exception as e:
        print(f"invalid: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()

import sys
import nbformat

def main():
    notebook_json = sys.stdin.read()
    try:
        nb = nbformat.reads(notebook_json, as_version=4)
        print("valid")
    except Exception as e:
        print("invalid")
        sys.exit(1)

if __name__ == "__main__":
    main()

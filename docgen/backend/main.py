from fastapi import FastAPI, UploadFile, File
from fastapi.middleware.cors import CORSMiddleware
import ast

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

def extract_docs(source_code: str):
    tree = ast.parse(source_code)
    result = []

    for node in ast.walk(tree):
        if isinstance(node, ast.ClassDef):
            class_doc = ast.get_docstring(node) or "No description available."
            methods = []

            for item in node.body:
                if isinstance(item, ast.FunctionDef):
                    params = [arg.arg for arg in item.args.args if arg.arg != "self"]
                    method_doc = ast.get_docstring(item) or "No description available."
                    methods.append({
                        "name": item.name,
                        "params": params,
                        "docstring": method_doc,
                        "line": item.lineno
                    })

            result.append({
                "type": "class",
                "name": node.name,
                "docstring": class_doc,
                "methods": methods,
                "line": node.lineno
            })

        elif isinstance(node, ast.FunctionDef):
            # skip if it belongs to a class (already captured above)
            params = [arg.arg for arg in node.args.args]
            func_doc = ast.get_docstring(node) or "No description available."
            result.append({
                "type": "function",
                "name": node.name,
                "params": params,
                "docstring": func_doc,
                "line": node.lineno
            })

    return result

@app.post("/analyze")
async def analyze(file: UploadFile = File(...)):
    contents = await file.read()
    try:
        source_code = contents.decode("utf-8")
        docs = extract_docs(source_code)
        return {"status": "success", "data": docs}
    except SyntaxError as e:
        return {"status": "error", "message": f"Syntax error in file: {str(e)}"}
    except Exception as e:
        return {"status": "error", "message": str(e)}

@app.get("/")
def root():
    return {"message": "DocGen backend is running"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="127.0.0.1", port=8000)
# Information Retrieval Project (Java)

This project implements a simple **Information Retrieval System** in Java, following the requirements of the *Information Retrieval* assignment.

It includes:
- **Inverted Index** stored on disk (`index.dict`, `docs.map`, `collection.freq`)
- **Retrieving Function** with AND / OR queries
- **Stop List** (manual file, can be empty) and **Stop Words by frequency**
- **Porter Stemmer** for normalization
- **Skip Pointers** for faster conjunctive queries
- **CLI** and **Swing GUI** for search

---

##  Compile

From the project root, compile all Java source files into the `out/` folder:

```bash
# 1. Compile
javac -d out src/ir/*.java

# 2. Index dataset (no frequency-based stop words)
java -cp out ir.Main index data stoplist.txt 0

# 3. Run AND query from CLI
java -cp out ir.Main search and "example query"

# 4. Launch GUI
java -cp out ir.Gui





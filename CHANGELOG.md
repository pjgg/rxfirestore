# CHANGELOG

## 1.0.1
### Bugfix
* Avoid noise timeout log error.

## 1.0.0
### Features
* Init API methods:
    - Insert: Insert create a Document with an auto-generate ID
    - Empty: Empty create a document for a given collection, and return an an auto-generate ID.
    - Upsert: If the document does not exist, it will be created. If the document does exist, its contents will be overwritten with the newly provided data.
    - Get: Get will retrieve a Document by ID for a given collection name.
    - Query Builder: Query builder allow you to develop your own query with where statement.
    - Run Query: get will retrieve a List of Documents by a given query.
    - Add Query Listener: You can listen to a document changes
    - Update: Update full document (overwrite).
    - Delete: To delete a document, use the delete method.

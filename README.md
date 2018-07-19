# boogle

The solution is implemented in Play with Scala. Used mongodb with reactivemongo. Apache solr was the first choice for the problem in hand for indexing pdf content. But went with mongodb for easy setup and execution.

Steps to run the program
1. Install mongo as standalone `brew install mongodb` or run it as docker container `docker run -d -p 27017:27107 -v ~/data:/data/db mongo`. Port 27017 used for connection
2. Run the application using gradle `./gradlew runPlayBinary`. The application starts listening on port 9000
3. Open postman any rest client for testing
4. Note that the filename is considered as book title and has to be unique for new pdf indexing

The supported functionality
1. Index pdf document. <br/>
URL => http://localhost:9000/book <br/>
method => POST <br/>
params => optional ?reIndex=true for reindexing the content <br/>
content => Any number of files as multipart form data. Only pdf file is supported and other files are ignored with warning in the response <br/>
response => Any warnings or errors as body. 200 on success with jobId to track and 400 for bad request <br/>

2. Query job status. <br/>
URL => http://localhost:9000/job/{jobId} <br/>
method => GET <br/>
param => pass jobId as part of url param <br/>
response => Job status. <br/>

3. Search book by quote. <br/>
URL => http://localhost:9000/book <br/>
method => GET <br/>
params => ?quote=quote from the book <br/>
response => List of records as json with title, content and pageNumber <br/>


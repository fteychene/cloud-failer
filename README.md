# Cloud failer

This application is a useless http server to fail after some random time.  
This could be use for some tests on application that fail or for some pedagogic learning.

Features :
 - The application start and fail in a randomly time between 30 and 300s.
 - The generated time is logged at startup.
 - At each startup the aplication generate a random UUID to log
 - Http `GET` on `/` display a landing page
 - Http `GET` on `/health` return a healthcheck and the UUID of the server
 - Http `POST` on `/fail` trigger a failure of the application 3 seconds after receiving the request
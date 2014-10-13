#!/usr/bin/env python
import web
import time
from threading import Semaphore

sem = Semaphore(0)

urls = (
    '/', 'Index', # GET
    '/add', 'Add', # POST
    '/comet', 'Comet', # GET
)

class Index:
    def GET(self):
        print "sem.release()"
        sem.release()
        return "sem.release()"
class Add:
    def POST(self):
        return "ADDED OK!"
class Comet:
    def GET(self):
        sem.acquire()
        return "got"
if __name__ == "__main__":
    app = web.application(urls, globals())
    app.run()


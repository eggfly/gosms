#!/usr/bin/env python

import tornado.ioloop
import tornado.web

class SmsQueue:
    def __init__(self):
        self.queue = []
    def dequeue(self):
        result = None
        if len(self.queue) > 0:
            result = self.queue.pop(0)
        return result
    def enqueue(self, sms):
        self.queue.append(sms)

workers = []
sms_queue = SmsQueue()

def send(handler, message):
    handler.write(message)
    handler.finish()
    workers.remove(handler)
class MainHandler(tornado.web.RequestHandler):
    def get(self):
        self.write("get /")
        print "get /"
        sms_queue.enqueue('**message**')
        if len(workers) > 0:
            handler = workers[0]
            msg = sms_queue.dequeue()
            if msg is not None:
                send(handler, msg)
class CometHandler(tornado.web.RequestHandler):
    @tornado.web.asynchronous
    def get(self):
        print "get /comet"
        workers.append(self)
    def on_connection_close(self):
        print "get /comet closed"
        workers.remove(self)
application = tornado.web.Application([
    (r"/comet", CometHandler),
    (r"/", MainHandler),
])

if __name__ == "__main__":
    application.listen(8080)
    tornado.ioloop.IOLoop.instance().start()

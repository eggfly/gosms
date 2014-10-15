#!/usr/bin/env python

import tornado.ioloop
import tornado.web
import logging

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
    def dump(self):
        logging.warn(self.queue)
workers = []
sms_queue = SmsQueue()
def try_send_one_message():
    if len(workers) > 0:
        handler = workers[0]
        msg = sms_queue.dequeue()
        sms_queue.dump()
        if msg is not None:
            send(handler, msg)
def send(handler, message):
    handler.write(message)
    handler.finish()
    workers.remove(handler)
    print "workers after send:", workers
class MainHandler(tornado.web.RequestHandler):
    def get(self):
        self.write("mainpage")
class TestHandler(tornado.web.RequestHandler):
    def get(self):
        self.write("get /test")
        print "get /test"
        sms_queue.enqueue('**message**')
        sms_queue.dump()
        try_send_one_message()
class CometHandler(tornado.web.RequestHandler):
    @tornado.web.asynchronous
    def get(self):
        print "get /comet"
        workers.append(self)
        print "workers after append:", workers
        try_send_one_message()
    def on_connection_close(self):
        print "get /comet closed"
        workers.remove(self)
    print "workers after send:", workers
application = tornado.web.Application([
    (r"/", MainHandler),
    (r"/comet", CometHandler),
    (r"/test", TestHandler),
])

if __name__ == "__main__":
    application.listen(8080)
    tornado.ioloop.IOLoop.instance().start()

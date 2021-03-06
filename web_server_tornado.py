#!/usr/bin/env python

import tornado.ioloop
import tornado.web
import logging
import os
import json
import db
class SmsModel:
    def __init__(self):
        pass
    def fetch(self):
        item, count = db.fetch_sms_task() 
        if item:
            sms_id = item[0]
            number = item[1]
            message = item[2]
            item = {'type': 'sendsms', 'id': sms_id, 'number': number, 'message': message}
        return item
    def enqueue(self, user_id, cmd_str):
        cmd = json.loads(cmd_str)
        cmd_type = cmd['type']
        number = cmd['number']
        msg = cmd['message']
        # TODO user_id
        result = db.add_new_sms(user_id, number, msg)
        return result
    def set_sms_sent_to_worker(self, worker_info, sms_id):
        db.set_sms_sent_to_worker(worker_info, sms_id)
    def dump(self):
        pass
    def auth(self, username, password):
        return db.auth(username, password)
workers = []
model = SmsModel()
def try_send_one_message():
    if len(workers) > 0:
        handler = workers[0]
        msg = model.fetch()
        model.dump()
        if msg is not None:
            send(handler, msg)
def send(handler, msg):
    handler.write(json.dumps(msg))
    handler.finish()
    workers.remove(handler)
    print "workers after send:", workers
    worker_info = str(vars(handler.request))
    model.set_sms_sent_to_worker(worker_info, msg['id'])
    print "set_sms_sent_to_worker complete!"
class MainHandler(tornado.web.RequestHandler):
    def get(self):
        # self.write("mainpage")
        self.render('index.html')
class SendHandler(tornado.web.RequestHandler):
    def post(self):
        # self.write("get /test")
        print "get /send"
        number = self.get_argument('number', '', True)
        message = self.get_argument('message', '', True)
        user = self.get_argument('user', '')
        password = self.get_argument('password', '')
        if not number or not user or not password:
            self.write('invalid params')
            self.set_status(400)
            return
        ok, bundle = model.auth(user, password)
        if not ok:
            bundle['result'] = ok
            self.write(json.dumps(bundle))
            return
        user_id = bundle['user_id']
        sms = {'type': 'sendsms', 'number': number, 'message': message}
        sms_json = json.dumps(sms)
        ok, bundle = model.enqueue(user_id, sms_json)
        model.dump()
        try_send_one_message()
        bundle['result'] = ok
        self.write(json.dumps(bundle))
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
        print "workers after on_connection_close:", workers
application = tornado.web.Application(
    handlers = [
        (r"/", MainHandler),
        (r"/comet", CometHandler),
        (r"/send", SendHandler),
    ], template_path=os.path.join(os.path.dirname(__file__), "templates")
)

if __name__ == "__main__":
    port = 8080
    application.listen(port)
    print "listening on port:", port
    tornado.ioloop.IOLoop.instance().start()

#!/usr/bin/env python

import logging
class SmsMemQueue:
    def __init__(self):
        self.queue = []
    def fetch(self):
        result = None
        if len(self.queue) > 0:
            result = self.queue.pop(0)
        return result
    def enqueue(self, sms):
        self.queue.append(sms)
    def dump(self):
        logging.warn(self.queue)

#!/usr/bin/env python

"""
sms db
"""

import sqlite3
import os
import shutil

STATE_UNKNOWN = 0
STATE_SERVER_PENDING = 1
STATE_WORKER_GOT = 2
STATE_WORKER_SENT = 3
STATE_REMOTE_DELIVERED = 4
STATE_REMOTE_REPLIED = 5

DEFAULT_DB = 'default.db'
SMS_DB = "sms.db"
if not os.path.isfile(SMS_DB) and os.path.isfile(DEFAULT_DB):
    shutil.copy(DEFAULT_DB, SMS_DB)
conn = sqlite3.connect('sms.db')
c = conn.cursor()

def add_new_sms(user_id, to_address, message):
    c.execute("INSERT INTO sms (user_id, to_address, message, status) VALUES (?, ?, ?, ?)", (user_id, to_address, message, STATE_SERVER_PENDING))
    result = c.lastrowid
    conn.commit()
    return result > 0
def get_sms_task():
    return None
def set_sms_sent_to_worker(sms_id):
    return True
if __name__ == '__main__':
    add_new_sms(2, '+8618601065423', '\x00\x01\x02TEST\x60')

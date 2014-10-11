#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
sms db
"""

import sqlite3
import os
import shutil

STATE_UNKNOWN = 0
STATE_SERVER_QUEUING = 1
STATE_SENT_TO_WORKER = 2
STATE_WORKER_SENT_TO_REMOTE = 3
STATE_REMOTE_DELIVERED = 4
STATE_REMOTE_REPLIED = 5

DEFAULT_DB = 'default.db'
SMS_DB = "sms.db"
if not os.path.isfile(SMS_DB) and os.path.isfile(DEFAULT_DB):
    shutil.copy(DEFAULT_DB, SMS_DB)
conn = sqlite3.connect('sms.db')
c = conn.cursor()

def add_new_sms(user_id, to_address, message):
    c.execute("INSERT INTO sms (user_id, to_address, message, status) VALUES (?, ?, ?, ?)",
        (user_id, to_address, message, STATE_SERVER_QUEUING))
    conn.commit()
    return c.lastrowid
def fetch_sms_task():
    c.execute("SELECT id, to_address, message FROM sms WHERE status = ? order by add_time", (STATE_SERVER_QUEUING,))
    return c.fetchone(), c.rowcount
def set_sms_sent_to_worker(worker_info, sms_id):
    c.execute("UPDATE sms SET status = ?, worker_info = ? WHERE id = ?", (STATE_SENT_TO_WORKER, worker_info, sms_id))
    result = c.rowcount == 1
    conn.commit()
    return result
if __name__ == '__main__':
    print add_new_sms(2, '+8618601065423', u'TEST你好unicode\x00\x40\x70')
    result = fetch_sms_task()
    print result
    sms_id = result[0][0]
    print set_sms_sent_to_worker("ip", sms_id)

#!/usr/bin/env python

"""
sms db
"""

import sqlite3

STATE_UNKNOWN = 0
STATE_PENDING = 1
STATE_WORKER_GOT = 2
STATE_WORKER_SENT = 3
STATE_DELIVERED = 4

conn = sqlite3.connect('sms.db')
c = conn.cursor()
CREATE_SQL = '''CREATE TABLE IF NOT EXISTS [sms] (
  [id] INTEGER PRIMARY KEY, 
  [user] TEXT, 
  [ip] TEXT, 
  [from_address] TEXT, 
  [to_address] TEXT, 
  [message] TEXT, 
  [add_time] DATETIME NOT NULL DEFAULT (datetime('now','localtime')), 
  [send_time] DATETIME, 
  [receive_time] DATETIME, 
  [worker] TEXT);
'''
c.execute(CREATE_SQL)
def add_new_sms(to_address, message):
    c.execute("INSERT INTO sms (to_address, message) VALUES (?, ?)", (to_address, message))
    result = c.lastrowid
    conn.commit()
    return result > 0
def get_sms_task():
    return None
def set_sms_sent_to_worker(sms_id):
    return True
if __name__ == '__main__':
    add_new_sms('+8618601065423', '\x00\x01\x02TEST\x60')

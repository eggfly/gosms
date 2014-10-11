#!/usr/bin/env python

"""
sms db
"""

import sqlite3
conn = sqlite3.connect('sms.db')
c = conn.cursor()
CREATE_SQL = '''CREATE TABLE [sms] (
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


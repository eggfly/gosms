#!/usr/bin/env python

"""
sms db
"""

import sqlite3
conn = sqlite3.connect('sms.db')
c = conn.cursor()
CREATE_SQL = '''create table if not exists sms(requester text, ip text, from_address text, to_address text, message text, add_time datetime, sent_time datetime, receive_time datetime, worker text)'''
c.execute(CREATE_SQL)


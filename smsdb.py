#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
sms db
"""

import sqlite3
import os
import shutil
import time
import datetime

STATE_UNKNOWN = 0
STATE_SERVER_QUEUING = 1
STATE_SENT_TO_WORKER = 2
STATE_WORKER_SENT_TO_REMOTE = 3
STATE_REMOTE_DELIVERED = 4
STATE_REMOTE_REPLIED = 5

FREE_TIER_COUNT = 10
SEND_INTERVAL_SECONDS = 5

DEFAULT_DB = 'default.db'
SMS_DB = 'sms.db'
if not os.path.isfile(SMS_DB) and os.path.isfile(DEFAULT_DB):
    shutil.copy(DEFAULT_DB, SMS_DB)
conn = sqlite3.connect('sms.db')
c = conn.cursor()
def auth(username, password):
    c.execute('SELECT id FROM user WHERE name = ? AND password = ?', (username, password))
    user = c.fetchone()
    if user:
        return True, {'user_id': user[0]}
    else: 
        return False, {'msg': 'username or password are not match'}
def add_new_sms(user_id, to_address, message):
    blocked, bundle = is_blocked(user_id, to_address, message)
    if blocked: return False, bundle
    # user existance already checked by is_blocked function
    c.execute("INSERT INTO sms (user_id, to_address, message, status) VALUES (?, ?, ?, ?)",
        (user_id, to_address, message, STATE_SERVER_QUEUING))
    conn.commit()
    success = c.rowcount == 1
    return success, {'id': c.lastrowid, 'msg': 'ok' if success else "error"}
def is_blocked(user_id, to_address, message):
    c.execute("SELECT antispam FROM user WHERE id = ?", (user_id,))
    user = c.fetchone()
    if user is None: return True, {'msg': "user_id not found"}
    antispam_enabled = user[0] != 0
    if not antispam_enabled: return False, {'msg': 'antispam setting of current user is disabled'}
    # need antispam
    c.execute('SELECT COUNT(*) FROM sms WHERE user_id = ?', (user_id,))
    count = c.fetchone()[0]
    if count > FREE_TIER_COUNT:
        # temporary blocked when total sms count is much
        return True, {'msg': 'cannot send more than %d messages in free account, please contact webadmin' %FREE_TIER_COUNT}
    c.execute('SELECT to_address, message, add_time FROM sms WHERE user_id = ? order by add_time DESC', (user_id,))
    latest = c.fetchone()
    blocked = False
    if latest:
        latest_time = latest[2]
        latest_time = time.strptime(latest_time, "%Y-%m-%d %H:%M:%S")
        diff = datetime.datetime.now() - datetime.datetime.fromtimestamp(time.mktime(latest_time))
        blocked = diff < datetime.timedelta(seconds=SEND_INTERVAL_SECONDS)
    interval_msg = "blocked, reason: cannot send sms within %d seconds" %SEND_INTERVAL_SECONDS
    return blocked, {"msg": interval_msg if blocked else "ok"}
def fetch_sms_task():
    c.execute("SELECT id, to_address, message FROM sms WHERE status = ? order by add_time", (STATE_SERVER_QUEUING,))
    return c.fetchone(), c.rowcount
def set_sms_sent_to_worker(worker_info, sms_id):
    c.execute("UPDATE sms SET status = ?, worker_info = ? WHERE id = ?", (STATE_SENT_TO_WORKER, worker_info, sms_id))
    result = c.rowcount == 1
    conn.commit()
    return result
if __name__ == '__main__':
    result = add_new_sms(2, '+8618601065423', u'TEST你好unicode\x00\x40\x70')
    print "add_new_sms result:", result
    result = fetch_sms_task()
    print "fetch_sms_task result:", result
    item = result[0]
    if item:
        sms_id = item[0]
        print set_sms_sent_to_worker("ip", sms_id)
        # c.execute("DELETE FROM sms WHERE id = ?", (sms_id,))
        # conn.commit()
    else:
        print "fetch_sms_task failed"

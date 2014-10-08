#!/usr/bin/env python

"""
my sms proxy server
"""

import select
import socket
import sys
import json

host = '' 
port = 6666
backlog = 5
size = 1024
server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server.bind((host, port))
server.listen(backlog)
# inputs = [server, sys.stdin]
inputs = [server,]

socket_map = {}
queue = []
worker_socket = None
def identify(s, data):
    global socket_map, worker_socket
    identity = data.strip()
    if identity == "WORKER":
        worker_socket = s
        return
    elif identity == "REQUESTER":
        socket_map[s] = ["REQUESTER", ]
        return
def command(s, data):
    global socket_map, queue
    identity = socket_map[s][0]
    cmd_str = data.strip()
    if identity == "REQUESTER":
        cmd = json.loads(cmd_str)
        cmd_type = cmd['type']
        number = cmd['number']
        msg = cmd['message']
        queue.append(cmd_str)
        worker_online = worker_socket is not None
        worker_status = "ONLINE" if worker_online else "OFFLINE"
        s.send('SERVER_GOT_COMMAND: %s, STATUS: %s\n' %(cmd, worker_status))
        if worker_online:
            for item in queue:
                worker_socket.send(cmd_str+'\n')
            queue = []
        else:
            print "worker_socket is not present! saved into queue, result: %s" %queue
def worker_response(data):
    pass
def disconnect(s):
    global socket_map, worker_socket
    if worker_socket == s:
        worker_socket = None
        print "worker_socket removed: %s" %s
    elif socket_map.has_key(s):
        item = socket_map[s]
        del socket_map[s]
        print "socket removed from map: %s, %s" %(s, item)
# main
while True:
    inputready, outputready, exceptready = select.select(inputs, [], [])
    # print "len(inputready): %s" %len(inputready)
    for s in inputready:
        if s == server:
            # handle the server socket
            client, address = server.accept()
            # client.settimeout(3)
            print "peer connected %s:%s" %address
            inputs.append(client)
        elif s == sys.stdin:
            # handle standard input 
            # junk = sys.stdin.readline()
            # running = 0
            pass
        else:
            # handle all other sockets
            # print dir(s)
            f = s.makefile()
            data = f.readline()
            # data = s.recv(size)
            if data:
                print "received: %s" %repr(data)
                if s == worker_socket:
                    worker_response(s, data)
                if socket_map.has_key(s):
                    command(s, data)
                else:
                    identify(s, data)
            else:
                # EOF: remote connection closed
                s.close()
                inputs.remove(s)
                disconnect(s)
                print "remote socket closed: " + str(s)
server.close()


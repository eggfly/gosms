#!/usr/bin/env python

"""
my sms proxy server
"""

import select
import socket
import sys

host = '' 
port = 6666
backlog = 5
size = 1024
server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.bind((host, port))
server.listen(backlog)
# inputs = [server, sys.stdin]
inputs = [server,]
while True:
    inputready, outputready, exceptready = select.select(inputs, [], [])
    print "len(inputready): %s" %len(inputready)
    for s in inputready:
        if s == server:
            # handle the server socket
            client, address = server.accept()
            # client.settimeout(3)
            # print dir(client)
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
                s.send(data)
            else:
                s.close()
                print dir(s)
                inputs.remove(s)
                print "closed: " + str(s)
server.close()

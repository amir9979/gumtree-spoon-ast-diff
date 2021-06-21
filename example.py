import socket

HOST = 'localhost'  # The server's hostname or IP address
PORT = 12345        # The port used by the server

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.connect((HOST, PORT))
    s.sendall(b'c:\\temp\\before.java;c:\\temp\\after.java;c:\\temp\\o.json\n')
    data = s.recv(1024)


with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.connect((HOST, PORT))
    s.sendall(b'break\n')
    data = s.recv(1024)

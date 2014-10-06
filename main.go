package main

import(
    "net"
    "fmt"
    "io"
)

const RECV_BUF_LEN = 1024

func main() {
    const ADDR = "0.0.0.0:6666"
    listener, err := net.Listen("tcp", ADDR)
    if err != nil {
        panic("error listening:"+err.Error())
    }
    fmt.Println("Starting the server: " + ADDR)

    for{
        conn, err := listener.Accept()
        if err != nil {
            panic("Error accept:" + err.Error())
        }
        fmt.Println("Accepted the Connection :", conn.RemoteAddr())
        go EchoServer(conn)
    }
}

func EchoServer(conn net.Conn) {
    buf := make([]byte, RECV_BUF_LEN)
    defer conn.Close()

    for {
        n, err := conn.Read(buf);
        switch err {
            case nil:
                conn.Write( buf[0:n] )
            case io.EOF:
                fmt.Printf("Warning: End of data: %s \n", err);
                return
            default:
                fmt.Printf("Error: Reading data : %s \n", err);
                return
        }
     }
}

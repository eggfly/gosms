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
        panic("error listening: " + err.Error())
    }
    fmt.Println("Starting the server: " + ADDR)

    for{
        conn, err := listener.Accept()
        if err != nil {
            panic("Error accept: " + err.Error())
        }
        fmt.Println("Accepted the Connection: ", conn.RemoteAddr())
        go GatewayServer(conn)
    }
}

func GatewayServer(conn net.Conn) {
    buf := make([]byte, RECV_BUF_LEN)
    stage := 0
    defer conn.Close()
    init(conn)
    stage = 1
    for {
        n, err := conn.Read(buf);
        switch err {
            case nil:
		// conn.Write(buf[0:n])
		handleNext(buf[0:n], conn, stage)
            case io.EOF:
                fmt.Printf("Warning: End of data: %s \n", err);
                return
            default:
                fmt.Printf("Error: Reading data: %s \n", err);
                return
        }
     }
}
func init(conn net.Conn) {
	err := conn.Write([]byte("I:GATEWAYPROXYWAITFORPASS\n"))
	if err != nil {
		panic(err)
	}
}
func handleNext(data []byte, conn net.Conn, stage int) {
	switch stage {
	    case 1: // wait pass
		str := string(data)
		fmt.Println(str)
		if str != 'eggfly' {
		    conn.Close()
		    return
		}
		conn.Write([]byte("I:AUTHOK\n"))
		return
	    case 2: // incoming or outcoming
		go incoming(conn net.Conn)
		outcoming(conn net.Conn)
		return
	}
}
func incoming(conn net.Conn) {
}
func outcoming(conn net.Conn) {
}

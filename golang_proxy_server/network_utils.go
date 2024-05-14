package yurisa

//package main

import (
	"fmt"
	"net"
)

// ExternalIP returns the external facing IP address of the host
func ExternalIP() string {
	ifaces, err := net.Interfaces()
	if err != nil {
		return "localhost"
	}

	for _, iface := range ifaces {
		if iface.Flags&net.FlagUp == 0 {
			continue // interface down
		}
		if iface.Flags&net.FlagLoopback != 0 {
			continue // loopback interface
		}
		addrs, err := iface.Addrs()
		if err != nil {
			return "localhost"
		}
		for _, addr := range addrs {
			var ip net.IP
			switch v := addr.(type) {
			case *net.IPNet:
				ip = v.IP
			case *net.IPAddr:
				ip = v.IP
			}
			if ip == nil || ip.IsLoopback() {
				continue
			}
			ip = ip.To4()
			if ip != nil {
				return ip.String()
			}
		}
	}
	return "localhost"
}

// It returns false if the port is not in use (able to bind), and true if the port is in use.
func CheckPort(port int) bool {
	addr := fmt.Sprintf("127.0.0.1:%d", port)
	listener, err := net.Listen("tcp", addr)
	if err != nil { //used
		return true
	}
	listener.Close()
	return false //not use
}

func SimpleIP() string {
	conn, err := net.Dial("udp", "8.8.8.8:80")
	if err != nil {

		fmt.Println("Simle IP Error:", err)
		return ""
	}
	defer conn.Close()

	localAddr := conn.LocalAddr().(*net.UDPAddr)
	return localAddr.IP.String()
}

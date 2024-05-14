package yurisa

import (
	"context"
	"encoding/json"
	"log"
	"net/http"
	"net/url"
	"strings"
	"time"

	"github.com/elazarl/goproxy"
)

var (
	//blackList = []string{}
	blockUpdate bool
	urlMap      = map[string]string{}
	server      *http.Server
)

func InitProxy() *goproxy.ProxyHttpServer {
	proxy := goproxy.NewProxyHttpServer()
	proxy.Verbose = false
	proxy.OnRequest().DoFunc(MapRequest)
	proxy.OnRequest().HandleConnectFunc(func(host string, ctx *goproxy.ProxyCtx) (*goproxy.ConnectAction, string) {
		if blockUpdate && strings.Contains(host, "gs-sec.ww.np.dl.playstation.net") {
			return goproxy.RejectConnect, host
		}
		return goproxy.OkConnect, host
	})
	return proxy
}

func SetMode(mode int, jsonLink string) {
	if mode == 1 {
		jsonMapping, err := Mapper(jsonLink)
		log.Printf("jsonMapping: %v", jsonMapping)
		if err != nil {
			log.Printf("[SetMode]Error getting URL mapping: %v", err)
			return
		}
		var mapping map[string]string
		err = json.Unmarshal([]byte(jsonMapping), &mapping)
		if err != nil {
			log.Printf("[SetMode]Error unmarshalling JSON mapping: %v", err)
			return
		}
		//UPDATE urlMap
		lastURL := mapping["lastJSON"]
		targetURL := mapping["targetJSON"]
		urlMap[lastURL] = targetURL
		log.Printf("[SetMode]MAP RULE: %v", urlMap)
	} else if mode == 2 {
		blockUpdate = true
	}
}

type ProxyInfo struct {
	IP   string
	Port string
}

func StartProxy(port string) {
	if server != nil {
		StopProxy()
	}

	ip := ExternalIP()
	proxy := InitProxy()
	address := ip + ":" + port

	server = &http.Server{
		Addr:    address,
		Handler: proxy,
	}

	go func() {
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("[StartProxy] Failed to start proxy server: %v", err)
		}
	}()
	log.Printf("[StartProxy]Proxy server started on %s", address)
	log.Printf("[StartProxy]blockUpdate: %v\n urlMap: %v", blockUpdate, urlMap)
}

func StopProxy() {
	if server == nil {
		log.Println("No server is currently running.")
		return
	}
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	if err := server.Shutdown(ctx); err != nil {
		log.Fatalf("[StopProxy]Failed to shutdown proxy server: %v", err)
	} else {
		log.Println("[StopProxy]Proxy server stopped successfully.")
	}
	server = nil
	blockUpdate = false
	urlMap = make(map[string]string)
	log.Printf("[StopProxy]Reset blockUpdate and urlMap to initial values\n blockUpdate: %v\n urlMap: %v", blockUpdate, urlMap)
}

func MapRequest(req *http.Request, ctx *goproxy.ProxyCtx) (*http.Request, *http.Response) {
	fullURL := req.URL.String()               // URL (including '?' parameters) captured from the console request
	baseURL := strings.Split(fullURL, "?")[0] //no ?

	// URL Mapping
	if newURL, ok := urlMap[baseURL]; ok {
		parsedNewURL, err := url.Parse(newURL)
		if err != nil {
			log.Printf("[MapAndBlock] Error parsing new URL: %s\n", err)
			return req, nil
		}
		req.URL = parsedNewURL
		log.Printf("[URL mapping successful] '%s' has been mapped to '%s'", baseURL, newURL)
		return req, nil
	}

	// Since blocking is handled at the proxy initialization based on the mode, no need to block here
	return req, nil
}

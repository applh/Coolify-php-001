package main

import (
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"runtime"
	"strings"
)

func getSitesFromContent(contentPath string) []string {
	var sites []string
	entries, err := os.ReadDir(contentPath)
	if err != nil {
		return sites
	}

	for _, entry := range entries {
		name := entry.Name()
		if name == "." || name == ".." || name == "my-data" {
			continue
		}
		if entry.IsDir() {
			sites = append(sites, name)
		}
	}
	return sites
}

func getPlugins() []string {
	var plugins []string
	pluginPath := "plugins"
	entries, err := os.ReadDir(pluginPath)
	if err != nil {
		return plugins
	}

	for _, entry := range entries {
		name := entry.Name()
		if name == "." || name == ".." {
			continue
		}
		if entry.IsDir() {
			plugins = append(plugins, name)
		}
	}
	return plugins
}

func fileExists(filename string) bool {
	info, err := os.Stat(filename)
	if os.IsNotExist(err) {
		return false
	}
	return !info.IsDir()
}

func getAdminPasskey() string {
	passkey := os.Getenv("APP_ADMIN_PASSKEY")
	if passkey == "" {
		passkey = os.Getenv("app_admin_passkey")
	}
	return passkey
}

func verifyAdmin(r *http.Request) bool {
	expectedPasskey := getAdminPasskey()
	if expectedPasskey == "" {
		return false
	}

	passkey := r.Header.Get("X-Admin-Passkey")
	if passkey == "" {
		passkey = r.Header.Get("x-admin-passkey")
	}
	if passkey == "" {
		cookie, err := r.Cookie("admin_passkey")
		if err == nil {
			passkey = cookie.Value
		}
	}
	return passkey == expectedPasskey
}

func main() {
	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}

	http.Handle("/js/", http.StripPrefix("/js/", http.FileServer(http.Dir("../repo-php/public/js"))))

	http.HandleFunc("/admin/api/sites", func(w http.ResponseWriter, r *http.Request) {
		if !verifyAdmin(r) {
			w.Header().Set("Content-Type", "application/json")
			w.WriteHeader(http.StatusForbidden)
			json.NewEncoder(w).Encode(map[string]string{"error": "Unauthorized"})
			return
		}
		
		sites := getSitesFromContent("content")
		data := map[string]interface{}{
			"status": "success",
			"sites":  sites,
		}
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(data)
	})

	http.HandleFunc("/admin/api/forms", func(w http.ResponseWriter, r *http.Request) {
		if !verifyAdmin(r) { w.WriteHeader(http.StatusForbidden); return }
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{"status": "success", "forms": []interface{}{}})
	})

	http.HandleFunc("/admin/api/forms/save", func(w http.ResponseWriter, r *http.Request) {
		if !verifyAdmin(r) { w.WriteHeader(http.StatusForbidden); return }
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{"status": "success"})
	})

	http.HandleFunc("/admin/api/forms/delete", func(w http.ResponseWriter, r *http.Request) {
		if !verifyAdmin(r) { w.WriteHeader(http.StatusForbidden); return }
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{"status": "success"})
	})

	http.HandleFunc("/admin/api/forms/submissions", func(w http.ResponseWriter, r *http.Request) {
		if !verifyAdmin(r) { w.WriteHeader(http.StatusForbidden); return }
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{"status": "success", "submissions": []interface{}{}})
	})

	http.HandleFunc("/admin", func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "admin.html")
	})

	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Query().Get("cms_debug") == "true" {
			contentPath := "content"
			isWritable := false

			testFile := filepath.Join(contentPath, ".write_test")
			if err := os.WriteFile(testFile, []byte(""), 0644); err == nil {
				isWritable = true
				os.Remove(testFile)
			}

			data := map[string]interface{}{
				"content_path":      contentPath,
				"is_writable":       isWritable,
				"sites":             getSitesFromContent(contentPath),
				"available_plugins": getPlugins(),
				"server_software":   "net/http",
				"go_version":        runtime.Version(),
			}
			w.Header().Set("Content-Type", "application/json")
			json.NewEncoder(w).Encode(data)
			return
		}

		hostName := strings.Split(r.Host, ":")[0]
		siteOverride := r.URL.Query().Get("__site")
		if siteOverride == "" {
			siteOverride = os.Getenv("ACTIVE_SITE_OVERRIDE")
		}

		activeSite := ""
		sites := getSitesFromContent("content")
		
		for _, s := range sites {
			if s == hostName || s == siteOverride {
				activeSite = s
				break
			}
		}

		if activeSite == "" && len(sites) > 0 {
			activeSite = sites[0]
		}
		if activeSite == "" {
			activeSite = "site1.com"
		}

		contentPath := filepath.Join("content", activeSite)
		urlPath := r.URL.Path
		if urlPath == "/" {
			urlPath = "/index.html"
		}
		
		fullPath := filepath.Join(contentPath, urlPath)
		if !fileExists(fullPath) {
			fullPath = filepath.Join("content", activeSite, "index.html")
			if !fileExists(fullPath) {
				http.NotFound(w, r)
				return
			}
		}

		http.ServeFile(w, r, fullPath)
	})

	fmt.Printf("Server starting on port %s...\n", port)
	log.Fatal(http.ListenAndServe(":"+port, nil))
}

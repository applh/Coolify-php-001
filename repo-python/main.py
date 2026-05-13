from fastapi import FastAPI, Request, HTTPException
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates
import json
import os

app = FastAPI()

# Setup templates
templates = Jinja2Templates(directory="templates")

# Load sites data
def load_sites():
    with open("sites.json", "r", encoding="utf-8") as f:
        return json.load(f)

@app.get("/", response_class=HTMLResponse)
async def read_site(request: Request):
    sites = load_sites()
    
    # Domain detection
    host = request.headers.get("host", "").split(":")[0]
    site_override = request.query_params.get("__site")
    
    active_site = None
    for site in sites:
        if site["domain"] == host or site["id"] == site_override:
            active_site = site
            break
            
    # Fallback
    if not active_site:
        active_site = sites[0]
        
    return templates.TemplateResponse("site.html", {"request": request, "site": active_site})

@app.get("/api/sites")
async def get_sites():
    return load_sites()

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=3000)

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

@app.get("/favicon.ico", include_in_schema=False)
async def favicon():
    return HTMLResponse(content="", status_code=204)

@app.get("/", response_class=HTMLResponse)
async def read_site(request: Request):
    try:
        sites = load_sites()
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to load sites: {str(e)}")
    
    # Domain detection
    host = request.headers.get("host", "").split(":")[0]
    site_override = request.query_params.get("__site")
    
    active_site = next(
        (s for s in sites if s["domain"] == host or s["id"] == site_override),
        sites[0] if sites else None
    )
    
    if not active_site:
        raise HTTPException(status_code=404, detail="No sites configured")
        
    return templates.TemplateResponse(
        request=request,
        name="site.html",
        context={"site": active_site}
    )

@app.get("/api/sites")
async def get_sites():
    return load_sites()

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=3000)

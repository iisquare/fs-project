import gc
from contextlib import asynccontextmanager
from enum import Enum
from pathlib import Path
from typing import Optional, List, Union, Dict

import torch
import uvicorn
import yaml
from fastapi import FastAPI, Depends, HTTPException, routing, APIRouter
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer


class Server:

    def __init__(self, args: Dict) -> None:
        self.args = args
        self.api_host = args.get("api_host", "0.0.0.0")
        self.api_port = args.get("api_port", 6006)
        self.api_prefix = args.get("api_prefix", "")
        self.api_keys = args['api_keys'].split(",") if 'api_keys' in args and args['api_keys'] else None
        self.log_config = args.get("log_config", "config/log_uvicorn.yaml")
        self.log_config = yaml.safe_load(Path(self.log_config).read_text())
        self.app = FastAPI(lifespan=self.lifespan)
        self.app.add_middleware(
            CORSMiddleware,
            allow_origins=["*"],
            allow_credentials=True,
            allow_methods=["*"],
            allow_headers=["*"],
        )

    def instance(self):
        return self.app

    def arguments(self):
        return self.args

    def include_router(self, router: routing.APIRouter, tags: Optional[List[Union[str, Enum]]],) -> None:
        self.app.include_router(router, prefix=self.api_prefix, tags=tags)

    @staticmethod
    def gc() -> None:
        r"""
        Collects GPU memory.
        """
        gc.collect()
        if torch.cuda.is_available():
            torch.cuda.empty_cache()
            torch.cuda.ipc_collect()

    @asynccontextmanager
    async def lifespan(self, app: "FastAPI"):  # collects GPU memory
        yield
        self.gc()

    async def check_api_key(self,
                            auth: Optional[HTTPAuthorizationCredentials] = Depends(HTTPBearer(auto_error=False)),):
        if not self.api_keys:
            # api_keys not set; allow all
            return None
        if auth is None or (token := auth.credentials) not in self.api_keys:
            raise HTTPException(
                status_code=401,
                detail={
                    "error": {
                        "message": "",
                        "type": "invalid_request_error",
                        "param": None,
                        "code": "invalid_api_key",
                    }
                },
            )
        return token

    def run(self):
        uvicorn.run(self.app, host=self.api_host, port=self.api_port, log_config=self.log_config)


def main(args: Dict):
    app = Server(args)
    router = APIRouter()
    if args.get("embedding_name_or_path"):
        from fs_gpt.api import embedding
        app.include_router(embedding.create_router(app, router, args), tags=["Embedding"])
    if args.get("classify_name_or_path"):
        from fs_gpt.api import classify
        app.include_router(classify.create_router(app, router, args), tags=["Classify"])
    app.run()

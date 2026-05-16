from typing import TYPE_CHECKING, Dict

import tiktoken
from fastapi import APIRouter, Depends, status

from fs_gpt.embedding.RAGEmbedding import RAGEmbedding
from fs_gpt.protocol.embedding import EmbeddingCreateParams

if TYPE_CHECKING:
    from fs_gpt.api.server import Server


def create_router(app: "Server", router: APIRouter, args: Dict) -> APIRouter:
    print(f"Load embedding model: {args.get('embedding_name_or_path')}")
    rag = RAGEmbedding(args.get('embedding_name_or_path'), args.get("embedding_device"), args.get("embedding_batch_size", 32))
    embedding_size = args.get("embedding_size", -1)

    @router.post(
        "/embeddings",
        dependencies=[Depends(app.check_api_key)],
        status_code=status.HTTP_200_OK,
    )
    @router.post(
        "/engines/{model_name}/embeddings",
    )
    async def create_embeddings(request: EmbeddingCreateParams, model_name: str = None,):
        """Creates embeddings for the text"""
        if request.model is None:
            request.model = model_name

        request.input = request.input
        if isinstance(request.input, str):
            request.input = [request.input]
        elif isinstance(request.input, list):
            if isinstance(request.input[0], int):
                decoding = tiktoken.model.encoding_for_model(request.model)
                request.input = [decoding.decode(request.input)]
            elif isinstance(request.input[0], list):
                decoding = tiktoken.model.encoding_for_model(request.model)
                request.input = [decoding.decode(text) for text in request.input]

        request.dimensions = request.dimensions or embedding_size

        return rag.encode(
            texts=request.input,
            model=request.model,
            encoding_format=request.encoding_format,
            dimensions=request.dimensions,
        )
    return router

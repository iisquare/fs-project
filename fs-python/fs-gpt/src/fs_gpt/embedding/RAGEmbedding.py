import base64
from typing import List, Literal, Optional

import numpy as np
from sentence_transformers import SentenceTransformer
from sentence_transformers.util import normalize_embeddings

from fs_gpt.protocol.embedding import Embedding, CreateEmbeddingResponse, Usage


class RAGEmbedding:

    def __init__(
            self,
            model_name_or_path: str,
            device: str = None,
            batch_size: int = 32,
    ) -> None:
        self.device = device
        self.batch_size = batch_size
        self.model = SentenceTransformer(model_name_or_path, trust_remote_code=True, device=self.device,)

    def encode(
        self,
        texts: List[str],
        model: Optional[str] = "",
        encoding_format: Literal["float", "base64"] = "float",
        dimensions: Optional[int] = -1,
    ) -> CreateEmbeddingResponse:
        dim = self.model.get_sentence_embedding_dimension()
        use_matryoshka = bool(0 < dimensions < dim)

        data, total_tokens = [], 0
        batches = [texts[i: i + 1024] for i in range(0, len(texts), 1024)]
        for num_batch, batch in enumerate(batches):
            vecs = self.model.encode(
                batch,
                batch_size=self.batch_size,
                normalize_embeddings=False if use_matryoshka else True,
                convert_to_tensor=True if use_matryoshka else False,
            )

            bs = vecs.shape[0]
            if dimensions > dim:
                zeros = np.zeros((bs, dimensions - dim))
                vecs = np.c_[vecs, zeros]
            elif 0 < dimensions < dim:
                vecs = vecs[..., :dimensions]  # Shrink the embedding dimensions
                vecs = normalize_embeddings(vecs).cpu().numpy()

            if encoding_format == "base64":
                vecs = [base64.b64encode(v.tobytes()).decode("utf-8") for v in vecs]
            else:
                vecs = vecs.tolist()

            data.extend(
                Embedding(
                    index=num_batch * 1024 + i,
                    object="embedding",
                    embedding=embedding,
                )
                for i, embedding in enumerate(vecs)
            )
            total_tokens += sum(len(i) for i in batch)

        return CreateEmbeddingResponse(
            data=data,
            model=model,
            object="list",
            usage=Usage(prompt_tokens=total_tokens, total_tokens=total_tokens),
        )

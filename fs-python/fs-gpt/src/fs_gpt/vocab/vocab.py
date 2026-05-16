import os
from pathlib import Path
from typing import Dict

import sentencepiece
from transformers import AutoTokenizer, LlamaTokenizer
from transformers.utils.sentencepiece_model_pb2 import ModelProto


class Vocab:
    def __init__(self, args: Dict):
        self.args = args
        self.stage = args.get("stage")
        self.output_dir = args.get("output_dir", f"logs/vocab")

    def train(self):
        if not os.path.exists(self.output_dir):
            os.makedirs(self.output_dir)
        model_prefix = str(Path(self.output_dir).joinpath("tokenizer").absolute())
        sentencepiece.SentencePieceTrainer.Train(
            input=self.args.get("input"),
            input_format=self.args.get("input_format", ""),
            model_prefix=model_prefix,
            # 训练语料内容的长度，不能少于词表大小，提示Vocabulary size too high异常
            vocab_size=self.args.get("vocab_size", 512),
            # 指定模型所支持的语言列表，语言代码是ISO639标准定义的缩写，如"en,zh"
            accept_language=self.args.get("accept_language", ""),
            character_coverage=self.args.get("character_coverage", 1.0),
            # UTF-8中一个汉字3个字节，对应.txt一行最多1397个汉字。
            max_sentence_length=self.args.get("max_sentence_length", 4192),
            # 是否将所有数字字符拆分为单独的单元，如”123“拆成”1“，”2“，”3“子词单元
            split_digits=self.args.get("split_digits", True),
            # 在遇到未知或很少的字符时将其分解为 UTF-8 字节来表示，启用后BPE实现的效果就和BBPE一样了
            byte_fallback=self.args.get("byte_fallback", True),
            user_defined_symbols=self.args.get("user_defined_symbols", None),
            model_type=self.args.get("model_type", "bpe"),
        )

    def expansion(self):
        print(f"load model tokenizer from {self.args.get('model_name_or_path')}")
        tokenizer = AutoTokenizer.from_pretrained(self.args.get("model_name_or_path"))
        if not isinstance(tokenizer, LlamaTokenizer):
            print(f"https://github.com/QwenLM/Qwen/blob/main/tokenization_note.md#vocabulary-expansion")
            return
        model_spm = ModelProto()
        model_spm.ParseFromString(tokenizer.sp_model.serialized_model_proto())

        print(f"load processor tokenizer from {self.args.get('tokenizer_path')}")
        processor = sentencepiece.SentencePieceProcessor()
        processor.Load(self.args.get("tokenizer_path"))
        processor_spm = ModelProto()
        processor_spm.ParseFromString(processor.serialized_model_proto())

        # print number of tokens
        print(f"tokenizer length: {len(tokenizer)}, processor length: {len(processor)}")
        print(f"tokenizer.all_special_tokens: {tokenizer.all_special_tokens}")
        print(f"tokenizer.all_special_ids: {tokenizer.all_special_ids}")
        print(f"tokenizer.special_tokens_map: {tokenizer.special_tokens_map}")

        # Add tokens to model tokenizer
        spm_tokens_set = set(p.piece for p in model_spm.pieces)
        print(f"spm_tokens_set length: {len(spm_tokens_set)}")
        print(f"model pieces length before: {len(model_spm.pieces)}")
        for p in processor_spm.pieces:
            piece = p.piece
            if piece not in spm_tokens_set:
                new_p = ModelProto().SentencePiece()
                new_p.piece = piece
                new_p.score = 0
                model_spm.pieces.append(new_p)
        print(f"model pieces length after: {len(model_spm.pieces)}")
        # Save
        print(f"save tokenizer to {self.output_dir}")
        tokenizer.save_pretrained(self.output_dir)
        tokenizer_path = str(Path(self.output_dir).joinpath("tokenizer_expansion.model").absolute())
        with open(tokenizer_path, 'wb') as f:
            f.write(model_spm.SerializeToString())
        print(f"Done.")

    def tokenize(self):
        print(f"load model tokenizer from {self.args.get('model_name_or_path')}")
        tokenizer = AutoTokenizer.from_pretrained(self.args.get("model_name_or_path"))
        # print number of tokens
        print(f"tokenizer length: {len(tokenizer)}")
        print(f"tokenizer.all_special_tokens: {tokenizer.all_special_tokens}")
        print(f"tokenizer.all_special_ids: {tokenizer.all_special_ids}")
        print(f"tokenizer.special_tokens_map: {tokenizer.special_tokens_map}")
        while True:
            text = input("Input: ")
            if not text:
                break
            result = tokenizer(text)
            print(f"tokenizer: {result}")
            input_ids = result["input_ids"]
            print(f"decode input_ids: {tokenizer.decode(input_ids)}")
            print(f"convert_ids_to_tokens: {tokenizer.convert_ids_to_tokens(input_ids)}")
            tokens = tokenizer.tokenize(text)
            print(f"tokenize: {tokens}")
            print(f"text length: {len(text)}")
            print(f"tokenize length: {len(tokens)}")
        print(f"Bye!")

    def piece(self):
        match self.stage:
            case "train":
                self.train()
            case "expansion":
                self.expansion()
            case "tokenize":
                self.tokenize()
            case _:
                raise Exception(f"Unknown stage: {self.stage}")

def main(args: Dict):
    Vocab(args).piece()

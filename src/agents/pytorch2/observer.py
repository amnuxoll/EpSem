from collections import deque
import torch
import random

class Observer:
    """
    Originally DFAAgent in Yuji's code, renamed for clarity.

    Keeps the last n observations (ints in 0..K), and encodes them
    as a concatenation of one-hots => vector dim = n_hist * (K+1).

    A "one-hot" is a vector (an array of numbers) used in machine
    learning to represent categorical data, where exactly one
    element is set to 1 and all other elements are 0.
    #TODO: Put this into our own Java agent
    """
    def __init__(self, n_hist: int, K: int, seed: int = 0):
        self.n_hist = int(n_hist)
        self.K = int(K)
        self.dim_slot = K + 1  # token vocab: 0..K
        self.hist = deque(maxlen=self.n_hist)
        random.seed(seed)

    def reset(self):
        self.hist.clear()
        for _ in range(self.n_hist):
            self.hist.append(0)  # 0 = "no previous action yet"

    def observe(self, obs_token: int):
        """Push the latest env observation token (int in 0..K)."""
        self.hist.append(obs_token)

    def encode(self) -> torch.Tensor:
        """Return 1D float tensor of size n_hist * (K+1)."""
        x = torch.zeros(self.n_hist, self.dim_slot, dtype=torch.float32)
        for i, tok in enumerate(self.hist):
            if 0 <= tok < self.dim_slot:
                x[i, tok] = 1.0
        return x.flatten()  # [n_hist * (K+1), ]
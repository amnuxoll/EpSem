from collections import deque
import torch
import random

class DFAObserver:
    """
    Originally DFAAgent in Yuji's code, renamed for clarity.

    Keeps the last n observations (ints in 0..K), and encodes them
    as a concatenation of one-hots => vector dim = n_hist * (K+1).

    A "one-hot" is a vector (an array of numbers) used in machine
    learning to represent categorical data, where exactly one
    element is set to 1 and all other elements are 0.

    Yuji's observer assumes that an observation is an action token and
    this is based on K + 1 where  K is the number of actions. To incorporate
    our sensors, we are removing this and replacing it with language that
    incorporates our number of sensors. 
    """
    def __init__(self, n_hist: int, n_sensors: int, K: int, seed: int = 0):
        self.n_hist = int(n_hist)

        self.n_sensors = int(n_sensors) # added sensor data

        self.K = int(K) # number of actions, TODO: remove K, unused
        # self.dim_slot = K + 1  # token vocab: 0..K
        self.dim_slot = 1 + n_sensors # token vocab: 
        self.hist = deque(maxlen=self.n_hist)
        random.seed(seed)

    def reset(self):
        self.hist.clear()
        for _ in range(self.n_hist):
            self.hist.append([0] + [0] * self.n_sensors)  # 0 = "no previous action yet"

    def observe(self, obs_token: int, sensor_values):
        """Push the latest env observation token (int in 0..K) and sensor values."""
        if len(sensor_values) != self.n_sensors:
            raise ValueError(
                f"Expected {self.n_sensors} sensors, "
                f"got {len(sensor_values)}"
            )

        self.hist.append(
            [obs_token] + list(sensor_values)
        )

    def encode(self) -> torch.Tensor:
        # Previous code that just sent the observation token:
        # """Return 1D float tensor of size n_hist * (K+1)."""
        # x = torch.zeros(self.n_hist, self.dim_slot, dtype=torch.float32)
        # for i, tok in enumerate(self.hist):
        #     if 0 <= tok < self.dim_slot:
        #         x[i, tok] = 1.0
        # return x.flatten()  # [n_hist * (K+1), ]

        x = torch.tensor(
            list(self.hist),
            dtype=torch.float32
        )

        return x.flatten()

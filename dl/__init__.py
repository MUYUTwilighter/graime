import platform

import numpy as np
import torch as torch
from tqdm import tqdm
from numpy import ndarray
from torch.nn import Conv1d, MaxPool1d, BatchNorm1d, ReLU, Flatten, Linear, MSELoss, Sigmoid
from torch.nn.modules.loss import _Loss, KLDivLoss, L1Loss, MarginRankingLoss
from torch.optim import Optimizer, Adam
from torch.utils.data import DataLoader, TensorDataset

TOTAL_WORDS: int = 30000
TRAIN_WORDS: int = int(0.7 * TOTAL_WORDS)
BATCH_SIZE: int = 50
BATCHES = int(TRAIN_WORDS / BATCH_SIZE)

TRAIN_WORDS = BATCHES * BATCH_SIZE


def switch_device() -> str:
    if torch.cuda.is_available():
        torch.cuda.set_device('cuda:0')
        name = torch.cuda.get_device_name('cuda:0')
        device = 'cuda:0'
    else:
        name = platform.processor()
        device = 'cpu'
    print(f"Current computing device: {name}")
    return device


def read_data(count: int) -> tuple[ndarray, int]:
    data: ndarray = np.load("resources/frequencies.npy")[:count]
    return data, data[0].size - 1


class Net(torch.nn.Module):
    def __init__(self, batch_size: int, _word_size: int, loss: _Loss, _device: str) -> None:
        super().__init__()
        self.__optimizer: Optimizer = None
        self.__loss: _Loss = loss
        self.__batch_size = batch_size
        self.__word_size = _word_size
        word_size_p1 = _word_size - int(_word_size / 3) + 1
        word_size_p2 = word_size_p1 - int(word_size_p1 / 2) + 1
        word_size_p3 = word_size_p2 - int(word_size_p2 / 3) + 1
        word_size_p4 = word_size_p3 - int(word_size_p3 / 2) + 1
        self.module = torch.nn.Sequential(
            Conv1d(1, 64, int(_word_size / 3)),
            MaxPool1d(int(word_size_p1 / 2), 1),
            BatchNorm1d(64),
            ReLU(),
            Conv1d(64, 128, int(word_size_p2 / 3)),
            MaxPool1d(int(word_size_p3 / 2), 1),
            BatchNorm1d(128),
            ReLU(),
            Conv1d(128, 32, word_size_p4),
            ReLU(),
            Flatten(),
            Linear(32, 64),
            ReLU(),
            Linear(64, 1),
            Flatten(),
            Sigmoid(),
            Linear(1, 1),
        )
        self.__loss.to(_device)
        self.to(_device)

    def set_optimizer(self, optimizer: Optimizer):
        self.__optimizer = optimizer

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        return self.module(x)

    def batch_train(self, trains: torch.Tensor, labels: torch.Tensor) -> tuple[float, float]:
        _train_loss: float = 0
        trains = trains.reshape(trains.size(0), 1, self.__word_size)
        self.train()
        for i in tqdm(range(trains.size(0) // self.__batch_size)):
            batch_train = trains[i * self.__batch_size: (i + 1) * self.__batch_size]
            batch_label = labels[i * self.__batch_size: (i + 1) * self.__batch_size]
            predict = self(batch_train)
            feature_loss = self.__loss(predict, batch_label)
            self.__optimizer.zero_grad()
            feature_loss.backward()
            self.__optimizer.step()
            _train_loss += feature_loss.item()

        self.eval()
        with torch.no_grad():
            _train_acc = 1 - MarginRankingLoss()(self(trains), labels)
        return _train_loss / (trains.size(0) // self.__batch_size), _train_acc

    def test(self, tests: torch.Tensor, labels: torch.Tensor) -> float:
        self.eval()
        tests = tests.reshape(tests.size(0), 1, self.__word_size)
        delta = abs((labels[1:] - labels[:-1]).sum().item() / labels.size(0))
        with torch.no_grad():
            results = self(tests).flatten()
            return


def run() -> None:
    # Set device
    device = switch_device()

    # Process data
    data, word_size = read_data(TOTAL_WORDS)
    data = torch.tensor(data, device=device).type(torch.float32)
    sequences = data[:, :-1]
    sequences /= 0x9fa6
    frequencies = data[:, -1]

    # Construct model
    net = Net(BATCH_SIZE, word_size, L1Loss(), device)
    net.set_optimizer(Adam(net.parameters()))

    # Train Model
    train_loss, train_acc = net.batch_train(sequences[:TRAIN_WORDS], frequencies[:TRAIN_WORDS])

    # Test Model
    test_acc = net.test(sequences[TRAIN_WORDS:], frequencies[TRAIN_WORDS:])

    print(f'''
Train Loss: {train_loss},
Train Accuracy: {train_acc},

Test Accuracy: {test_acc}
''')


if __name__ == "__main__":
    run()
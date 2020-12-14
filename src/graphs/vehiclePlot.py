"""
Creation of graph G21 with vehicle stats imported from the csv file.
"""

import pandas as pd
import matplotlib.pyplot as plt

data_file = pd.read_csv('vehicle.csv')
data_file.plot()
plt.savefig('G21.png', dpi=1000)


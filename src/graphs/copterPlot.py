"""
Creation of graph G19-20 with ithakiCopter stats imported from the csv file.
"""

import pandas as pd
import matplotlib.pyplot as plt

data_file = pd.read_csv('ithakiCopter.csv')
data_file.plot()
plt.savefig('G19-20.png', dpi=1000)

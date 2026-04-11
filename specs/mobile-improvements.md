# Mobile Improvements

## Objectives

- The Mobile view will be updated independently of the desktop view. Changes to the mobile view should not have any side effects.
- The mobile view needs to be simplified. It cannot display all the data available in the desktop view.
- There should be a single Assist screen, currently there are 3

## Assist Mobile View

- It should show the number of remaining word / total words in the dictionary. All other metrics shown in the desktop view such as
Eliminated, Reduction, etc. are not required.
- The Count, Most Common Letters, and the matrix of word count by position and letter needs to be combined into a single panel and table that fits on a mobile screen
- Under the table heading, it should have the total letter column counts by position, but no graphics
- It should then have a table with 5 rows to condense the information in the Most Common Letters table and the full (complete alpohabet) table.
- The top row shows the most common letters and count. The next 4 rows show the next 4 most common letters. When a letter
is correctly identified or there are fewer than 5 remaining possibilities those cells are empty.
- The table headings are P1, P2, P3, P4, P5

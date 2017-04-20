(TeX-add-style-hook
 "report"
 (lambda ()
   (TeX-add-to-alist 'LaTeX-provided-class-options
                     '(("report" "paper=a4" "fontsize=11pt")))
   (TeX-add-to-alist 'LaTeX-provided-package-options
                     '(("fontenc" "T1") ("babel" "english") ("geometry" "margin=1.5in")))
   (TeX-run-style-hooks
    "latex2e"
    "rep10"
    "fontenc"
    "fourier"
    "graphicx"
    "booktabs"
    "tabularx"
    "babel"
    "amsmath"
    "amsfonts"
    "amsthm"
    "geometry"
    "lipsum"
    "sectsty"
    "fancyhdr")
   (TeX-add-symbols
    '("horrule" 1)))
 :latex)


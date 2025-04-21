import os
import cv2
import numpy as np
import matplotlib.pyplot as plt

def run_plot(image_path, files_dir):
    # 1) Leer imagen original y máscara generada por Kotlin
    img = cv2.imread(image_path)
    if img is None:
        raise FileNotFoundError(f"No se pudo cargar imagen: {image_path}")

    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

    mask_path = os.path.join(files_dir, "mask.png")
    mask = cv2.imread(mask_path, cv2.IMREAD_GRAYSCALE)
    if mask is None:
        raise FileNotFoundError(f"No se pudo cargar máscara: {mask_path}")

    # 2) Contornos
    contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL,
                                   cv2.CHAIN_APPROX_SIMPLE)

    # 3) Crear overlay a partir de gray
    overlay = cv2.cvtColor(gray, cv2.COLOR_GRAY2BGR)
    cv2.drawContours(overlay, contours, -1, (0,0,255), 2)

    # 4) Graficar y guardar
    fig, axs = plt.subplots(1,3, figsize=(15,5))
    axs[0].imshow(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))
    axs[0].set_title("Original");   axs[0].axis("off")
    axs[1].imshow(mask, cmap="gray")
    axs[1].set_title("Máscara");     axs[1].axis("off")
    axs[2].imshow(cv2.cvtColor(overlay, cv2.COLOR_BGR2RGB))
    axs[2].set_title("Contornos");   axs[2].axis("off")

    out_png = os.path.join(files_dir, "segmentation_plot.png")
    plt.savefig(out_png, bbox_inches="tight")
    plt.close(fig)

    return out_png
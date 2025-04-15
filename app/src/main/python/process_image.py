# process_image.py
import os
import cv2
import re
import pytesseract

def recortar_zona(img, x, y, w, h):
    return img[y:y+h, x:x+w]

def extraer_texto_ocr(img):
    return pytesseract.image_to_string(img)

def extraer_numeros(texto):
    # Buscar números enteros o flotantes en el string
    return re.findall(r'\d+(?:\.\d+)?', texto)

def ocr_range_t(image_path):
    # Definir dos zonas fijas para extraer temperaturas (ajusta estos valores según tu formato)
    # Suponiendo que la zona superior contiene la temperatura máxima
    # y la zona inferior la mínima.
    x_top, y_top, w_top, h_top = 100, 50, 150, 50     # Ajusta estas coordenadas
    x_bottom, y_bottom, w_bottom, h_bottom = 100, 200, 150, 50  # Ajusta estas coordenadas

    img = cv2.imread(image_path)
    if img is None:
        print("Error al cargar la imagen")
        return "", ""

    recorte_top = recortar_zona(img, x_top, y_top, w_top, h_top)
    recorte_bottom = recortar_zona(img, x_bottom, y_bottom, w_bottom, h_bottom)

    texto_top = extraer_texto_ocr(recorte_top)
    texto_bottom = extraer_texto_ocr(recorte_bottom)

    top_nums = extraer_numeros(texto_top)
    bottom_nums = extraer_numeros(texto_bottom)

    # Si no se detecta el valor, dejamos el string vacío para permitir input manual.
    max_temp = top_nums[0] if top_nums else ""
    min_temp = bottom_nums[0] if bottom_nums else ""

    print("OCR - Temp máxima:", max_temp, "Temp mínima:", min_temp)
    return max_temp, min_temp

def process_image_wrapper(image_path):
    max_temp, min_temp = ocr_range_t(image_path)
    return {"max_temp": max_temp, "min_temp": min_temp}

# Para pruebas locales, puedes descomentar:
# if __name__ == "__main__":
#     image_path = "ruta_a_tu_imagen.jpg"
#     resultados = process_image_wrapper(image_path)
#     print(resultados)
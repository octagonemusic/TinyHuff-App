import React, { useRef } from "react";

const Decompress = () => {
  const fileInput = useRef();

  const onFormSubmit = (event) => {
    event.preventDefault();

    const file = fileInput.current.files[0];
    if (!file) {
      console.error("No file selected");
      alert("No file selected");
      return;
    }

    // Check if the file is a .zip file
    if (file.type !== "application/zip") {
      console.error("Invalid file type");
      alert("Please select a .zip file");
      return;
    }

    const formData = new FormData();
    formData.append("file", file);

    fetch("http://localhost:8080/decompress", {
      method: "POST",
      body: formData,
    })
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(
            "Your ZIP file does not contain a .huff .ser and a .txt file. Please try again."
          );
        }

        // Check the content type of the response
        const contentType = response.headers.get("content-type");
        if (!contentType || !contentType.includes("application/octet-stream")) {
          // If the content type is not as expected, assume there's an error and don't download the file
          const error = await response.text();
          throw new Error(error);
        }

        return response.blob();
      })
      .then((blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.setAttribute("download", "output.txt");
        document.body.appendChild(link);
        link.click();
      })
      .catch((error) => {
        console.error("Error during API call", error);
        alert("An error occurred: " + error.message);
      });
  };

  return (
    <form onSubmit={onFormSubmit}>
      <input type="file" name="file" ref={fileInput} />
      <button type="submit">Decompress</button>
    </form>
  );
};

export default Decompress;

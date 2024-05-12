import React, { useRef } from "react";

const Compress = () => {
  const fileInput = useRef();

  const compressFile = async () => {
    const file = fileInput.current.files[0];
    if (!file) {
      console.error("No file selected");
      alert("No file selected");
      return;
    }

    // Check if the file is a .txt file
    if (file.type !== "text/plain") {
      console.error("Invalid file type");
      alert("Please select a .txt file");
      return;
    }

    const formData = new FormData();
    formData.append("file", file);

    const response = await fetch("http://localhost:8080/compress", {
      method: "POST",
      body: formData,
    });

    if (!response.ok) {
      throw new Error("Network response was not ok");
    }

    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.setAttribute("download", "compressed.zip");
    document.body.appendChild(link);
    link.click();
    link.parentNode.removeChild(link);
  };

  return (
    <div>
      <input type="file" ref={fileInput} />
      <button onClick={compressFile}>Compress</button>
    </div>
  );
};

export default Compress;

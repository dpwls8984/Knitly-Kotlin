"use client"; 

import { useEffect } from "react";
import { useRouter } from "next/navigation";


export default function EventPage() {
    const router = useRouter();
  
    useEffect(() => {
      alert("아직 구현되되지 않은 페이지 입니다.");
      router.back(); 
    }, [router]); 
    return null;
  }
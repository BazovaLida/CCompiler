.386
.model flat, stdcall
option casemap :none

include C:\masm32\include\kernel32.inc
include C:\masm32\include\user32.inc

includelib C:\masm32\lib\kernel32.lib
includelib C:\masm32\lib\user32.lib

 PROTO

.data
msg_title db "Result", 0
buffer db 128 dup(?)
format db "%d",0

.code
start:
	invoke 
	invoke wsprintf, addr buffer, addr format, eax
	invoke MessageBox, 0, addr buffer, addr msg_title, 0
	invoke ExitProcess, 0

 PROC
	push ebp
	mov ebp, esp

	push 3
	push 4
	pop eax ;here is the result
	mov esp, ebp  ; restore ESP; now it points to old EBP
	pop ebp       ; restore old EBP; now ESP is where it was before prologue
	ret
 ENDP
END start
